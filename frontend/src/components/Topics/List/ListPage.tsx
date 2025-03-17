import React, { Suspense } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ClusterNameRoute, clusterTopicNewRelativePath } from 'lib/paths';
import { PER_PAGE } from 'lib/constants';
import ClusterContext from 'components/contexts/ClusterContext';
import Search from 'components/common/Search/Search';
import { ActionButton } from 'components/common/ActionComponent';
import PageHeading from 'components/common/PageHeading/PageHeading';
import useAppParams from 'lib/hooks/useAppParams';
import { ControlPanelWrapper } from 'components/common/ControlPanel/ControlPanel.styled';
import Switch from 'components/common/Switch/Switch';
import PlusIcon from 'components/common/Icons/PlusIcon';
import PageLoader from 'components/common/PageLoader/PageLoader';
import TopicTable from 'components/Topics/List/TopicTable';
import { Action, ResourceType } from 'generated-sources';

const ListPage: React.FC = () => {
  const { isReadOnly } = React.useContext(ClusterContext);
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams, setSearchParams] = useSearchParams();

  // Set the search params to the url based on the localStorage value
  React.useEffect(() => {
    if (!searchParams.has('perPage')) {
      searchParams.set('perPage', String(PER_PAGE));
    }
    if (
      !!localStorage.getItem('hideInternalTopics') &&
      !searchParams.has('hideInternal')
    ) {
      searchParams.set('hideInternal', 'true');
    }
    setSearchParams(searchParams);
  }, []);

  const handleSwitch = () => {
    if (searchParams.has('hideInternal')) {
      localStorage.removeItem('hideInternalTopics');
      searchParams.delete('hideInternal');
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
      <PageHeading clusterName={clusterName} text="Topics">
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
      </PageHeading>
      <ControlPanelWrapper hasInput>
        <Search placeholder="Search by Topic Name" />
        <label>
          <Switch
            name="ShowInternalTopics"
            checked={!searchParams.has('hideInternal')}
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
