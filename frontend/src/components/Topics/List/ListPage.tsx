import React, { Suspense } from 'react';
import { useSearchParams } from 'react-router-dom';
import { clusterTopicNewRelativePath } from 'lib/paths';
import { PER_PAGE } from 'lib/constants';
import ClusterContext from 'components/contexts/ClusterContext';
import Search from 'components/common/Search/Search';
import { ActionButton } from 'components/common/ActionComponent';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Switch from 'components/common/Switch/Switch';
import PlusIcon from 'components/common/Icons/PlusIcon';
import PageLoader from 'components/common/PageLoader/PageLoader';
import TopicTable from 'components/Topics/List/TopicTable';
import { Action, ResourceType } from 'generated-sources';
import ResourcePageHeading from 'components/common/ResourcePageHeading/ResourcePageHeading';
import Fts from 'components/common/Fts/Fts';
import useFts from 'components/common/Fts/useFts';

const ListPage: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const [searchParams, setSearchParams] = useSearchParams();

  useFts('topics');

  // Set the search params to the url based on the localStorage value
  React.useEffect(() => {
    if (!searchParams.has('perPage')) {
      searchParams.set('perPage', String(PER_PAGE));
    }
    // If URL doesn't specify it, derive from localStorage (default = true when missing)
    if (!searchParams.has('hideInternal')) {
      const stored = localStorage.getItem('hideInternalTopics');
      const shouldHide = stored === null ? true : stored === 'true';
      searchParams.set('hideInternal', String(shouldHide));
      // persist the default so it sticks across pages
      if (stored === null) localStorage.setItem('hideInternalTopics', 'true');
    } else {
      // sync localStorage if URL has it set
      const raw = searchParams.get('hideInternal');
      const norm = raw === 'true' || raw === 'false' ? raw : 'true'; // default to true if malformed
      localStorage.setItem('hideInternalTopics', norm);
      if (norm !== raw) searchParams.set('hideInternal', norm); // sync URL if malformed
    }
    setSearchParams(searchParams);
  }, []);

  const handleSwitch = () => {
    if (searchParams.get('hideInternal') === 'true') {
      localStorage.setItem('hideInternalTopics', 'false');
      searchParams.set('hideInternal', 'false');
    } else {
      localStorage.setItem('hideInternalTopics', 'true');
      searchParams.set('hideInternal', 'true');
    }
    // Page must be reset when the switch is toggled
    searchParams.set('page', '1');
    setSearchParams(searchParams);
  };

  return (
    <>
      <ResourcePageHeading text="Topics">
        {!isReadOnly && (
          <ActionButton
            buttonType="primary"
            buttonSize="M"
            to={clusterTopicNewRelativePath}
            permission={{
              resource: ResourceType.TOPIC,
              action: Action.CREATE,
            }}
          >
            <PlusIcon /> Add a Topic
          </ActionButton>
        )}
      </ResourcePageHeading>
      <ControlPanelWrapper hasInput>
        <Search
          placeholder="Search by Topic Name"
          extraActions={<Fts resourceName="topics" />}
        />
        <label>
          <Switch
            name="ShowInternalTopics"
            checked={searchParams.get('hideInternal') === 'false'}
            onChange={handleSwitch}
          />
          Show Internal Topics
        </label>
      </ControlPanelWrapper>
      <Suspense fallback={<PageLoader />}>
        <TopicTable />
      </Suspense>
    </>
  );
};

export default ListPage;
